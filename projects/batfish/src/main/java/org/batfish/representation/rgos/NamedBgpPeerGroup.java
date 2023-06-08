package org.batfish.representation.rgos;

public class NamedBgpPeerGroup extends BgpPeerGroup {

  private String _name;

  public NamedBgpPeerGroup(String name) {
    _name = name;
  }

  @Override
  public String getName() {
    return _name;
  }
}
