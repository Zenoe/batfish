package org.batfish.vendor.check_point_management;

import java.io.Serializable;
import javax.annotation.Nonnull;

/** Abstract class representing a management object with a UID. */
public abstract class ManagementObject implements Serializable {

  protected static final String PROP_UID = "uid";

  protected ManagementObject(Uid uid) {
    _uid = uid;
  }

  public final @Nonnull Uid getUid() {
    return _uid;
  }

  protected boolean baseEquals(Object o) {
    if (this == o) {
      return true;
    } else if (!getClass().isInstance(o)) {
      return false;
    }
    ManagementObject that = (ManagementObject) o;
    return _uid.equals(that._uid);
  }

  protected int baseHashcode() {
    return _uid.hashCode();
  }

  private final @Nonnull Uid _uid;
}
