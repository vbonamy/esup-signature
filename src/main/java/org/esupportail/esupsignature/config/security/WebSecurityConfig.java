package org.esupportail.esupsignature.config.security;

import org.esupportail.esupsignature.config.security.cas.CasProperties;
import org.esupportail.esupsignature.config.security.otp.OtpAuthenticationProvider;
import org.esupportail.esupsignature.config.security.shib.DevClientRequestFilter;
import org.esupportail.esupsignature.config.security.shib.DevShibProperties;
import org.esupportail.esupsignature.config.security.shib.ShibProperties;
import org.esupportail.esupsignature.service.security.*;
import org.esupportail.esupsignature.service.security.cas.CasSecurityServiceImpl;
import org.esupportail.esupsignature.service.security.oauth.OAuthSecurityServiceImpl;
import org.esupportail.esupsignature.service.security.shib.ShibSecurityServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.oauth2.client.ClientsConfiguredCondition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.switchuser.SwitchUserFilter;
import org.springframework.security.web.session.ConcurrentSessionFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import javax.annotation.Resource;
import java.util.*;

@Configuration
@EnableWebSecurity(debug = false)
@EnableConfigurationProperties({WebSecurityProperties.class, ShibProperties.class, CasProperties.class, DevShibProperties.class})
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

	private static final Logger logger = LoggerFactory.getLogger(WebSecurityConfig.class);

	private static final String API_KEY_HEADER = "x-api-key";

	private String apiKey = "SomeKey1234567890";

	private LdapContextSource ldapContextSource;

	@Autowired(required = false)
	public void setLdapContextSource(LdapContextSource ldapContextSource) {
		this.ldapContextSource = ldapContextSource;
	}

	@Resource
	private WebSecurityProperties webSecurityProperties;

	private final List<SecurityService> securityServices = new ArrayList<>();

	private final List<DevSecurityFilter> devSecurityFilters = new ArrayList<>();

	@Bean
	@Order(1)
	@ConditionalOnProperty({"spring.ldap.base", "ldap.search-base", "security.cas.service"})
	public CasSecurityServiceImpl casSecurityServiceImpl() {
		if(ldapContextSource!= null && ldapContextSource.getUserDn() != null) {
			CasSecurityServiceImpl casSecurityService = new CasSecurityServiceImpl();
			securityServices.add(casSecurityService);
			return casSecurityService;
		} else {
			logger.error("cas config found without needed ldap config, cas security will be disabled");
			return null;
		}
	}

	@Bean
	@Order(2)
	@ConditionalOnProperty(prefix = "security.shib", name = "principal-request-header")
	public ShibSecurityServiceImpl shibSecurityServiceImpl() {
		ShibSecurityServiceImpl shibSecurityService = new ShibSecurityServiceImpl();
		securityServices.add(shibSecurityService);
		return shibSecurityService;
	}

	@Bean
	@Order(3)
	@Conditional(ClientsConfiguredCondition.class)
	public OAuthSecurityServiceImpl oAuthSecurityService() {
		OAuthSecurityServiceImpl oAuthSecurityService = new OAuthSecurityServiceImpl();
		securityServices.add(oAuthSecurityService);
		return oAuthSecurityService;
	}

	@Bean
	@Order(4)
	@ConditionalOnProperty(prefix = "security.shib.dev", name = "enable", havingValue = "true")
	public DevClientRequestFilter devClientRequestFilter() {
		DevClientRequestFilter devClientRequestFilter = new DevClientRequestFilter();
		devSecurityFilters.add(devClientRequestFilter);
		return devClientRequestFilter;
	}

	@Override
	public void configure(WebSecurity web) throws Exception {
		web.ignoring().antMatchers("/resources/**", "/webjars/**");
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		setAuthorizeRequests(http);
		http.antMatcher("/**").authorizeRequests().antMatchers("/").permitAll();
		devSecurityFilters.forEach(devSecurityFilter -> http.addFilterBefore(devSecurityFilter, OAuth2AuthorizationRequestRedirectFilter.class));
		http.exceptionHandling().defaultAuthenticationEntryPointFor(new IndexEntryPoint("/"), new AntPathRequestMatcher("/"));
		for(SecurityService securityService : securityServices) {
			http.antMatcher("/**").authorizeRequests().antMatchers(securityService.getLoginUrl()).authenticated();
			http.exceptionHandling().defaultAuthenticationEntryPointFor(securityService.getAuthenticationEntryPoint(), new AntPathRequestMatcher(securityService.getLoginUrl()));
			http.addFilterAfter(securityService.getAuthenticationProcessingFilter(), OAuth2AuthorizationRequestRedirectFilter.class);
			if(securityService.getClass().equals(OAuthSecurityServiceImpl.class)) {
				http.oauth2Client();
			}
		}
		for(SecurityService securityService : securityServices) {
			if(securityService instanceof CasSecurityServiceImpl) {
				switchUserFilter().setUserDetailsService(securityService.getUserDetailsService());
			} else if(securityService instanceof ShibSecurityServiceImpl) {
				switchUserFilter().setUserDetailsService(securityService.getUserDetailsService());
				break;
			}
		}
		http.logout()
				.logoutRequestMatcher(
						new AntPathRequestMatcher("/logout")
				)
				.addLogoutHandler(logoutHandler())
				.logoutSuccessUrl("/login").permitAll();
		http.sessionManagement().sessionAuthenticationStrategy(sessionAuthenticationStrategy()).maximumSessions(5).sessionRegistry(sessionRegistry());
		http.csrf()
				.ignoringAntMatchers("/ws/**")
				.ignoringAntMatchers("/user/nexu-sign/**")
				.ignoringAntMatchers("/otp/**")
				.ignoringAntMatchers("/log/**")
				.ignoringAntMatchers("/actuator/**")
				.ignoringAntMatchers("/h2-console/**");
		http.headers().frameOptions().sameOrigin();
		http.headers().disable();
	}

	@Bean
	public APIKeyFilter apiKeyFilter() {
		APIKeyFilter filter = new APIKeyFilter();
		filter.setAuthenticationManager(authentication -> {
			if(authentication.getPrincipal() == null) {
				throw new BadCredentialsException("Access Denied.");
			}
			String apiKey = (String) authentication.getPrincipal();
			if (authentication.getPrincipal() != null && this.apiKey.equals(apiKey)) {
				Collection<SimpleGrantedAuthority> oldAuthorities = (Collection<SimpleGrantedAuthority>) SecurityContextHolder.getContext().getAuthentication().getAuthorities();
				SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_WS");
				List<SimpleGrantedAuthority> updatedAuthorities = new ArrayList<>();
				updatedAuthorities.add(authority);
				updatedAuthorities.addAll(oldAuthorities);
				SecurityContextHolder.getContext().setAuthentication(
						new UsernamePasswordAuthenticationToken(
								SecurityContextHolder.getContext().getAuthentication().getPrincipal(),
								SecurityContextHolder.getContext().getAuthentication().getCredentials(),
								updatedAuthorities));
				return SecurityContextHolder.getContext().getAuthentication();
			} else {
				throw new BadCredentialsException("Access Denied.");
			}
		});
		return filter;
	}

	private void setAuthorizeRequests(HttpSecurity http) throws Exception {
		http.logout().logoutSuccessUrl("/").permitAll();
		AccessDeniedHandlerImpl accessDeniedHandlerImpl = new AccessDeniedHandlerImpl();
		accessDeniedHandlerImpl.setErrorPage("/denied");
		http.exceptionHandling().accessDeniedHandler(accessDeniedHandlerImpl);
		String hasIpAddresses = "";
		int nbIps = 0;
		if(webSecurityProperties.getWsAccessAuthorizeIps() != null) {
			for (String ip : webSecurityProperties.getWsAccessAuthorizeIps()) {
				nbIps++;
				hasIpAddresses += "hasIpAddress('"+ ip +"')";
				if(nbIps < webSecurityProperties.getWsAccessAuthorizeIps().length) {
					hasIpAddresses += " or ";
				}
			}
			http.authorizeRequests().antMatchers("/ws/**").access(hasIpAddresses);
			http.authorizeRequests().antMatchers("/actuator/**").access(hasIpAddresses);
//			http.authorizeRequests().antMatchers("/ws/**").access("hasRole('ROLE_WS')").and().addFilter(apiKeyFilter());
		} else {
			http.authorizeRequests().antMatchers("/ws/**").denyAll();
			http.authorizeRequests().antMatchers("/actuator/**").denyAll();
		}
		http.authorizeRequests().antMatchers("/otp/**").permitAll();
		http.authorizeRequests().antMatchers("/error").permitAll();
		http.authorizeRequests()
				.antMatchers("/").permitAll()
				.antMatchers("/admin/", "/admin/**").access("hasRole('ROLE_ADMIN')")
				.antMatchers("/user/", "/user/**").access("hasAnyRole('ROLE_USER', 'ROLE_OTP')")
				.antMatchers("/ws-secure/", "/ws-secure/**").access("hasAnyRole('ROLE_USER', 'ROLE_OTP')")
				.antMatchers("/public/", "/public/**").permitAll()
				.antMatchers("/h2-console/**").access("hasRole('ROLE_ADMIN')")
				.antMatchers("/webjars/**").permitAll();

	}

	@Bean
	public LogoutHandlerImpl logoutHandler() {
		return new LogoutHandlerImpl();
	}

	@Bean
	public SessionRegistryImpl sessionRegistry() {
		return new SessionRegistryImpl();
	}

	@Bean
	public ConcurrentSessionFilter concurrencyFilter() {
		return new ConcurrentSessionFilter(sessionRegistry());
	}

	@Bean
	public RegisterSessionAuthenticationStrategy sessionAuthenticationStrategy() {
		return new RegisterSessionAuthenticationStrategy(sessionRegistry());
	}

	@Bean
//	@ConditionalOnProperty({"spring.ldap.base", "ldap.search-base", "security.cas.service"})
	@ConditionalOnExpression("${global.enable-su}")
	public SwitchUserFilter switchUserFilter() {
		SwitchUserFilter switchUserFilter = new SwitchUserFilter();
		switchUserFilter.setUserDetailsService(new InMemoryUserDetailsManager());
		switchUserFilter.setSwitchUserUrl("/admin/su-login");
		switchUserFilter.setSwitchFailureUrl("/error");
		switchUserFilter.setExitUserUrl("/su-logout");
		switchUserFilter.setTargetUrl("/");
		return switchUserFilter;
	}

	@Bean
	@Override
	public AuthenticationManager authenticationManagerBean() {
		return new ProviderManager(List.of(new OtpAuthenticationProvider()));
	}

	@Bean
	public UserDetailsService userDetailsService() {
		return new InMemoryUserDetailsManager();
	}

	@Bean
	public SpelGroupService spelGroupService() {
		SpelGroupService spelGroupService = new SpelGroupService();
		Map<String, String> groups4eppnSpel = new HashMap<>();
		if (webSecurityProperties.getGroupMappingSpel() != null) {
			for (String groupName : webSecurityProperties.getGroupMappingSpel().keySet()) {
				String spelRule = webSecurityProperties.getGroupMappingSpel().get(groupName);
				groups4eppnSpel.put(groupName, spelRule);
			}
		}
		spelGroupService.setGroups4eppnSpel(groups4eppnSpel);
		return spelGroupService;
	}

}