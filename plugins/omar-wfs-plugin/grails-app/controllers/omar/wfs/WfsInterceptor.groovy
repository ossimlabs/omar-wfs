package omar.wfs


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

