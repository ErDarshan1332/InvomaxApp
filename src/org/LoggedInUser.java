package org;
public class LoggedInUser {
    private String empId;
    private String firstName;
    private String lastName;
    private String jobRole;

    public LoggedInUser(String empId, String firstName, String lastName, String jobRole) {
        this.empId = empId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.jobRole = jobRole;
    }

    // Getters
    public String getEmpId() { return empId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getJobRole() { return jobRole; }
}