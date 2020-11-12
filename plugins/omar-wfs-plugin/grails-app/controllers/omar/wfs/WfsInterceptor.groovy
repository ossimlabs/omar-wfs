package omar.wfs

import omar.core.BindUtil

class WfsInterceptor
{

  public WfsInterceptor()
  {

  }

  boolean before()
  {
    true
  }

  boolean after() { true }

  @Override
  void afterView() {
    // no-op
  }
}

