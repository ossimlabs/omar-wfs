package omar.wfs

import grails.validation.Validateable
import groovy.transform.ToString

/**
 * Created by sbortman on 9/22/15.
 */

@ToString( includeNames = true )
class GetFeatureRequest implements Validateable
{
  static mapWith = 'none'

  String username

  String service
  String version
  String request

  String typeName
  String namespace
  String filter
  String resultType
  String outputFormat
  String sortBy
  String propertyName

  Integer maxFeatures
  Integer startIndex

  String srsName
  
  static mapping = {
    version false
  }

  String toXML()
  {
    return ""
  }

  static GetFeatureRequest fromXML(String text)
  {
    def xml = new XmlSlurper().parseText( text )

    fromXML( xml )
  }

  static GetFeatureRequest fromXML(def xml)
  {
    def typeName = xml?.Query?.@typeName?.text()

    String specifiedVersion = WfsParseUtil.findVersion( xml )
    def maxFeatures = xml?.@maxFeatures?.text()
    def startIndex = xml?.@startIndex?.text()


    def propertyNames = xml?.Query?.first()?.PropertyName?.collect { it?.text()?.split( ':' )?.last() }?.join( ',' )


    def cmd = new GetFeatureRequest(
        service: 'WFS',
        version: specifiedVersion,
        request: 'GetFeature',
        typeName: typeName,
        outputFormat: xml?.@outputFormat?.text() ?: null,
        maxFeatures: ( maxFeatures ) ? maxFeatures?.toInteger() : null,
        startIndex: ( startIndex ) ? startIndex?.toInteger() : null,
        resultType: xml?.@resultType?.text(),
        filter: WfsParseUtil.getFilterAsString( xml ),
        sortBy: xml?.Query?.first()?.SortBy?.text(),
        propertyName: propertyNames
    )


    return cmd
  }
}
