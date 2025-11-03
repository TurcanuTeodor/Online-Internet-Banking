package ro.app.backend_Java_SpringBoot.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public class ContactInfoDTO {

    @Size(max = 20)
    private String phone;

    @Email
    @Size(max = 100)
    private String email;

    @Size(max = 100)
    private String contactPerson;

    @Size(max = 100)
    private String website;

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
}
