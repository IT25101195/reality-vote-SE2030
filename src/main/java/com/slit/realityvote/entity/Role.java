package com.slit.realityvote.entity;

/**
 * The six system roles identified during requirements gathering
 * (see Y2-S1-MLB-B1G1-10 requirements document, "For Use Case Diagrams").
 *
 * Each group member owns one major function tied to one of these roles:
 *   ADMINISTRATOR        -> Reality Show Management (this module)
 *   VIEWER                -> Registration, browsing contestants, casting votes
 *   CONTESTANT_STAFF      -> Contestant Management
 *   COMPLIANCE_OFFICER    -> Voting Compliance & Security / Audit logs
 *   REPORTING_MANAGER     -> Reports & Analytics
 *   SUPPORT_STAFF         -> Customer Support & Complaints
 *   JUDGE                 -> Judge & Panel Management (scoring side)
 */
public enum Role {
    ADMINISTRATOR,
    VIEWER,
    CONTESTANT_STAFF,
    COMPLIANCE_OFFICER,
    REPORTING_MANAGER,
    SUPPORT_STAFF,
    JUDGE
}
