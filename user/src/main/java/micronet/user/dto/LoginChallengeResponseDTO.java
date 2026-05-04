package micronet.user.dto;

public class LoginChallengeResponseDTO {
    private boolean requiresVerification;
    private String channel;
    private String message;

    public LoginChallengeResponseDTO() {
    }

    public LoginChallengeResponseDTO(boolean requiresVerification, String channel, String message) {
        this.requiresVerification = requiresVerification;
        this.channel = channel;
        this.message = message;
    }

    public boolean isRequiresVerification() {
        return requiresVerification;
    }

    public void setRequiresVerification(boolean requiresVerification) {
        this.requiresVerification = requiresVerification;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
