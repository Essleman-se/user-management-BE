package micronet.user.util;

import micronet.user.model.User;

public final class UserProfileUtils {

    private UserProfileUtils() {
    }

    /**
     * Splits a full display name (e.g. from Google) into first and last name.
     */
    public static void applyDisplayName(User user, String displayName) {
        if (displayName == null || displayName.isBlank()) {
            user.setFirstName("User");
            user.setLastName("");
            return;
        }
        String trimmed = displayName.trim();
        int space = trimmed.indexOf(' ');
        if (space < 0) {
            user.setFirstName(trimmed);
            user.setLastName("");
        } else {
            user.setFirstName(trimmed.substring(0, space).trim());
            user.setLastName(trimmed.substring(space).trim());
        }
    }
}
