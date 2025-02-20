package org.esupportail.esupsignature.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
public class SignRequest {
	
	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

	@Version
    private Integer version;
	
	@Column(unique=true)
	private String token;
	
	private String title;
	
    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm")
    private Date createDate;

    @ManyToOne
    private User createBy;

    private String exportedDocumentURI;

    @JsonIgnore
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true)
    @OrderColumn
    private List<Document> originalDocuments = new ArrayList<>();

    @JsonIgnore
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true)
    @OrderColumn
    private List<Document> signedDocuments = new ArrayList<>();

    @JsonIgnore
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true)
    @OrderColumn
    private List<Document> attachments = new ArrayList<>();

    @JsonIgnore
    @ElementCollection
    private List<String> links = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private SignRequestStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @NotNull
    private SignBook parentSignBook;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    @OrderColumn
    private List<SignRequestParams> signRequestParams = new LinkedList<>();

    private Date endDate;

    @OneToMany(cascade = CascadeType.REMOVE, orphanRemoval = true)
    @OrderColumn
    private List<Comment> comments = new ArrayList<>();

    @JsonIgnore
    @Transient
    transient Boolean signable = false;

    @JsonIgnore
    @Transient
    transient Boolean editable = false;

    @JsonIgnore
    @Transient
    transient Data data;

    private Date lastNotifDate;
    
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true)
    private Map<Recipient, Action> recipientHasSigned = new HashMap<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public User getCreateBy() {
        return createBy;
    }

    public void setCreateBy(User createBy) {
        this.createBy = createBy;
    }

    public String getExportedDocumentURI() {
        return exportedDocumentURI;
    }

    public void setExportedDocumentURI(String exportedDocumentURI) {
        this.exportedDocumentURI = exportedDocumentURI;
    }

    public List<Document> getOriginalDocuments() {
        return originalDocuments;
    }

    public void setOriginalDocuments(List<Document> originalDocuments) {
        this.originalDocuments = originalDocuments;
    }

    public List<Document> getSignedDocuments() {
        return signedDocuments;
    }

    public void setSignedDocuments(List<Document> signedDocuments) {
        this.signedDocuments = signedDocuments;
    }

    public List<Document> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<Document> attachments) {
        this.attachments = attachments;
    }

    public List<String> getLinks() {
        return links;
    }

    public void setLinks(List<String> links) {
        this.links = links;
    }

    public SignRequestStatus getStatus() {
        return status;
    }

    public void setStatus(SignRequestStatus status) {
        this.status = status;
    }

    public SignBook getParentSignBook() {
        return parentSignBook;
    }

    public void setParentSignBook(SignBook parentSignBook) {
        this.parentSignBook = parentSignBook;
    }

    public List<SignRequestParams> getSignRequestParams() {
        return signRequestParams;
    }

    public void setSignRequestParams(List<SignRequestParams> signRequestParams) {
        this.signRequestParams = signRequestParams;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    public Boolean getSignable() {
        return signable;
    }

    public void setSignable(Boolean signable) {
        this.signable = signable;
    }

    public Boolean getEditable() {
        return editable;
    }

    public void setEditable(Boolean editable) {
        this.editable = editable;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public Map<Recipient, Action> getRecipientHasSigned() {
        return recipientHasSigned;
    }

    @JsonIgnore
    public Map<Recipient, Action> getOrderedRecipientHasSigned() {
        if(recipientHasSigned.size() > 0) {
            Set<Map.Entry<Recipient, Action>> entries = recipientHasSigned.entrySet().stream().filter(recipientActionEntry -> recipientActionEntry.getValue() != null && recipientActionEntry.getValue().getDate() != null).sorted((o1, o2) -> o2.getValue().getDate().compareTo(o1.getValue().getDate())).collect(Collectors.toCollection(LinkedHashSet::new));
            Map<Recipient, Action> recipientActionMap = new LinkedHashMap<>();
            for (Map.Entry<Recipient, Action> entry : entries) {
                recipientActionMap.put(entry.getKey(), entry.getValue());
            }
            return recipientActionMap;
        } else {
            return recipientHasSigned;
        }
    }

    public void setRecipientHasSigned(Map<Recipient, Action> recipientHasSigned) {
        this.recipientHasSigned = recipientHasSigned;
    }

    @JsonIgnore
    public List<Document> getLiteOriginalDocuments() {
        List<Document> liteDocuments = new ArrayList<>();
        for (Document document : this.originalDocuments) {
            document.setBigFile(null);
            liteDocuments.add(document);
        }
        return liteDocuments;
    }

    @JsonIgnore
    public List<Document> getLiteSignedDocuments() {
        List<Document> liteDocuments = new ArrayList<>();
        for (Document document : this.signedDocuments) {
            document.setBigFile(null);
            liteDocuments.add(document);
        }
        return liteDocuments;
    }

    @JsonIgnore
    public Document getLastSignedDocument() {
        if(this.getSignedDocuments().size() > 0) {
            return this.getSignedDocuments().get(this.getSignedDocuments().size() - 1);
        } else {
            return getLastOriginalDocument();
        }
    }

    @JsonIgnore
    public Document getLastOriginalDocument() {
        List<Document> documents = this.getOriginalDocuments();
        if (documents.size() != 1) {
            return null;
        } else {
            return documents.get(0);
        }
    }

    public SignType getCurrentSignType() {
        if(this.getParentSignBook().getLiveWorkflow().getLiveWorkflowSteps() != null && this.getSignable()) {
            return this.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignType();
        } else {
            return null;
        }
    }

    public Date getLastNotifDate() {
        return lastNotifDate;
    }

    public void setLastNotifDate(Date lastNotifDate) {
        this.lastNotifDate = lastNotifDate;
    }


}
