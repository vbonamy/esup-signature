package org.esupportail.esupsignature.config.security;

import org.esupportail.esupsignature.config.security.otp.OtpAuthenticationProvider;
import org.esupportail.esupsignature.service.security.*;
import org.esupportail.esupsignature.service.security.cas.CasSecurityServiceImpl;
import org.esupportail.esupsignature.service.security.oauth.OAuthSecurityServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSecurity(debug = false)
@EnableConfigurationProperties(WebSecurityProperties.class)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

	@Resource
	private WebSecurityProperties webSecurityProperties;

	@Resource
	private List<SecurityService> securityServices;
	
	private List<DevSecurityFilter> devSecurityFilters;

	@Autowired(required = false)
	public void setDevSecurityFilters(List<DevSecurityFilter> devSecurityFilters) {
		this.devSecurityFilters = devSecurityFilters;
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		setAuthorizeRequests(http);
		http.antMatcher("/**").authorizeRequests().antMatchers("/").permitAll();
		if(devSecurityFilters != null) {
			devSecurityFilters.forEach(devSecurityFilter -> http.addFilterBefore(devSecurityFilter, OAuth2AuthorizationRequestRedirectFilter.class));
		}
		http.exceptionHandling().defaultAuthenticationEntryPointFor(new IndexEntryPoint("/"), new AntPathRequestMatcher("/"));
		for(SecurityService securityService : securityServices) {
			http.antMatcher("/**").authorizeRequests().antMatchers(securityService.getLoginUrl()).authenticated();
			http.exceptionHandling().defaultAuthenticationEntryPointFor(securityService.getAuthenticationEntryPoint(), new AntPathRequestMatcher(securityService.getLoginUrl()));
			http.addFilterAfter(securityService.getAuthenticationProcessingFilter(), OAuth2AuthorizationRequestRedirectFilter.class);
			if(securityService.getClass().equals(OAuthSecurityServiceImpl.class)) {
				http.oauth2Client();
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

	private void setAuthorizeRequests(HttpSecurity http) throws Exception {
		http.logout().logoutSuccessUrl("/").permitAll();
		AccessDeniedHandlerImpl accessDeniedHandlerImpl = new AccessDeniedHandlerImpl();
		accessDeniedHandlerImpl.setErrorPage("/denied");
		http.exceptionHandling().accessDeniedHandler(accessDeniedHandlerImpl);
		String hasIpAddresses = "";
		int nbIps = 0;
		if(webSecurityProperties.getWsAccessAuthorizeIps() == null) {
			hasIpAddresses = "	denyAll";
		} else {
			for (String ip : webSecurityProperties.getWsAccessAuthorizeIps()) {
				nbIps++;
				hasIpAddresses += "hasIpAddress('"+ ip +"')";
				if(nbIps < webSecurityProperties.getWsAccessAuthorizeIps().length) {
					hasIpAddresses += " or ";
				}
			}
		}
		http.authorizeRequests().antMatchers("/ws/**").access(hasIpAddresses);
		http.authorizeRequests().antMatchers("/actuator/**").access(hasIpAddresses);
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
		ConcurrentSessionFilter concurrentSessionFilter = new ConcurrentSessionFilter(sessionRegistry());
		return concurrentSessionFilter;
	}

	@Bean
	public RegisterSessionAuthenticationStrategy sessionAuthenticationStrategy() {
		RegisterSessionAuthenticationStrategy authenticationStrategy = new RegisterSessionAuthenticationStrategy(sessionRegistry());
		return authenticationStrategy;
	}

	@Bean
	@ConditionalOnProperty({"spring.ldap.base", "ldap.search-base", "security.cas.service"})
	@ConditionalOnExpression("${global.enable-su}")
	public SwitchUserFilter switchUserFilter() {
		SwitchUserFilter switchUserFilter = new SwitchUserFilter();
		for(SecurityService securityService : securityServices) {
			if(securityService instanceof CasSecurityServiceImpl) {
				switchUserFilter.setUserDetailsService(securityService.getUserDetailsService());
			}
		}
		switchUserFilter.setSwitchUserUrl("/admin/su-login");
		//switchUserFilter.setSwitchFailureUrl("/error");
		switchUserFilter.setExitUserUrl("/su-logout");
		switchUserFilter.setTargetUrl("/");
		return switchUserFilter;
	}

	@Bean
	@Override
	public AuthenticationManager authenticationManagerBean() {
		return new ProviderManager(Arrays.asList(new OtpAuthenticationProvider()));
	}

	@Bean
	public UserDetailsService userDetailsService() {
		InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager();
		return manager;
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