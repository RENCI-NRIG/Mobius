package org.renci.comet;

import java.util.Objects;

public class CometResponse {
    private int status;
    private String message = null;
    private String value = null;

    public CometResponse(int status) {
        this.status = status;
    }

    /**
     * COMET status code
     * @return status
     **/
    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    /**
     * COMET status message
     * @return message
     **/
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * JSON object
     * @return value
     **/
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }


    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CometResponse cometResponse = (CometResponse) o;
        return Objects.equals(this.status, cometResponse.status) &&
                Objects.equals(this.message, cometResponse.message) &&
                Objects.equals(this.value, cometResponse.value);
    }

    public int hashCode() {
        return Objects.hash(status, message, value);
    }


    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class CometResponse {\n");

        sb.append("    status: ").append(toIndentedString(status)).append("\n");
        sb.append("    message: ").append(toIndentedString(message)).append("\n");
        sb.append("    value: ").append(toIndentedString(value)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(java.lang.Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}
