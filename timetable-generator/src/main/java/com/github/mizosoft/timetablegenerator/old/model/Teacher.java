package com.github.mizosoft.timetablegenerator.old.model;

public final class Teacher {

  private final int id;
  private final String name;
  private final String phone;
  private final String email;

  private Teacher(int id, String name, String phone, String email) {
    this.id = id;
    this.name = name;
    this.phone = phone;
    this.email = email;
  }

  public int getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getPhone() {
    return phone;
  }

  public String getEmail() {
    return email;
  }

  @Override
  public String toString() {
    return "Teacher{" +
        "id=" + id +
        ", name='" + name + '\'' +
        ", phone='" + phone + '\'' +
        ", email='" + email + '\'' +
        '}';
  }

    public boolean isPhoneEmpty() {
        if (phone == null || phone.isBlank()) {
            return true;
        }
        return false;
    }

    public boolean isEmailEmpty() {
        if (email == null || email.isBlank()) {
            return true;
        }
        return false;
    }

  public static final class Builder {

    private int id;
    private String name;
    private String phone;
    private String email;

    public Builder setId(int id) {
      this.id = id;
      return this;
    }

    public Builder setName(String name) {
      this.name = name;
      return this;
    }

    public Builder setPhone(String phone) {
      this.phone = phone;
      return this;
    }

    public Builder setEmail(String email) {
      this.email = email;
      return this;
    }

    public Teacher build() {
      return new Teacher(id, name, phone, email);
    }
  }
}
