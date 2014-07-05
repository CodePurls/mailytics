package com.codepurls.mailytics.data.security;

import java.util.List;

import com.codepurls.mailytics.data.core.Mailbox;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class User {
  public int           id;
  public String        firstName, lastName, email;
  public List<Mailbox> mailboxes;
  public String        username;
  @JsonIgnore
  public String        phash;

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((email == null) ? 0 : email.hashCode());
    result = prime * result + id;
    result = prime * result + ((phash == null) ? 0 : phash.hashCode());
    result = prime * result + ((username == null) ? 0 : username.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    User other = (User) obj;
    if (email == null) {
      if (other.email != null) return false;
    } else if (!email.equals(other.email)) return false;
    if (id != other.id) return false;
    if (phash == null) {
      if (other.phash != null) return false;
    } else if (!phash.equals(other.phash)) return false;
    if (username == null) {
      if (other.username != null) return false;
    } else if (!username.equals(other.username)) return false;
    return true;
  }
}
