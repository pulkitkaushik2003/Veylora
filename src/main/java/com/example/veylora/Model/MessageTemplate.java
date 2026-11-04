package com.example.veylora.Model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** A saved SMS or email text. {{name}}, {{phone}} and {{email}} are filled in per recipient. */
@Entity
@Table(name = "message_templates")
public class MessageTemplate {

    public enum Channel { SMS, EMAIL }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Channel channel = Channel.SMS;

    /** Only used by EMAIL templates. */
    @Column(length = 300)
    private String subject;

    @Column(nullable = false, length = 5000)
    private String body;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Channel getChannel() { return channel; }
    public void setChannel(Channel channel) { this.channel = channel; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
