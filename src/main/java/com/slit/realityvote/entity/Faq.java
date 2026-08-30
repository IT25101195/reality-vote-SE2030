package com.slit.realityvote.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * A single FAQ entry, managed by Support Staff and shown publicly.
 * "The system should provide a FAQ section covering registration,
 * login, voting procedures, account verification, password recovery,
 * results, and troubleshooting guidance." (requirements doc)
 */
@Entity
@Table(name = "faqs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Faq {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Question is required")
    private String question;

    @NotBlank(message = "Answer is required")
    @Column(length = 2000)
    private String answer;

    @NotBlank(message = "Category is required")
    private String category; // e.g. Registration, Voting, Account
}
