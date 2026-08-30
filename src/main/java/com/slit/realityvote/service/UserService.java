package com.slit.realityvote.service;

import com.slit.realityvote.dto.PasswordChangeForm;
import com.slit.realityvote.dto.ProfileForm;
import com.slit.realityvote.dto.RegistrationForm;
import com.slit.realityvote.entity.Role;
import com.slit.realityvote.entity.User;

public interface UserService {

    /** True if an account with this email already exists (case-insensitive). */
    boolean emailTaken(String email);

    /**
     * Creates a new VIEWER account from a public sign-up submission.
     * Always assigns Role.VIEWER, regardless of what was posted - contestants
     * and staff/admin accounts are never created through this path.
     */
    User registerViewer(RegistrationForm form);

    /**
     * Creates a login account for a staff-side role (e.g. JUDGE) chosen by
     * an administrator - the counterpart to registerViewer for the
     * public sign-up path. Used by JudgeController so a newly added judge
     * can immediately sign in, the same way the seeded Demo Judge does.
     * @throws IllegalArgumentException if the email is already in use
     */
    User createStaffAccount(String fullName, String email, String rawPassword, Role role);

    /**
     * Updates the given user's full name / email from their own profile
     * page. Returns true if the email actually changed (the caller uses
     * this to decide whether the person needs to log back in, since the
     * email is also their login username).
     */
    boolean updateProfile(User user, ProfileForm form);

    /**
     * Changes the given user's password after verifying currentPassword
     * matches what's on file.
     * @throws IllegalArgumentException if the current password is wrong
     */
    void changePassword(User user, PasswordChangeForm form);
}
