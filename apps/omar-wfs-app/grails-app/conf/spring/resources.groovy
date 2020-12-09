// Place your Spring DSL code here
import omar.wfs.GeoscriptClientService
import omar.wfs.ExportClientProxyService

beans = {
  geoscriptService(GeoscriptClientService)
  exportClientService(ExportClientProxyService)
}
