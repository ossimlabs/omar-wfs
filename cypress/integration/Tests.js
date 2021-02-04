describe('Tests for omar-wfs', () => {
    let features, ids, minMax
    it('Grab up to 1000 images', () => {
        cy.request({method: "GET", url: "/wfs?filter=&outputFormat=JSON&request=GetFeature&service=WFS&sortBy=acquisition_date+D&startIndex=0&typeName=omar:raster_entry&version=1.1.0"})
            .then((response) => {
                expect(response.body).to.have.property('type', 'FeatureCollection')
                features = response.body.features
            })
    })
    it('Test for several filenames', () => {
        testRandom("filename")
    })
    it('Test for several image ids', () => {
        testRandom("image_id")
    })
    it('Grab all mission ids', () => {
        ids = []
        grabIds("mission_id", "mission_id%20IS%20NOT%20NULL")
    })
    it('Test for all mission ids', () => {
        testIds("mission_id")
    })
    it('Grab all product ids', () => {
        ids = []
        grabIds("product_id", "product_id%20IS%20NOT%20NULL")
    })
    it('Test for all product ids', () => {
        testIds("product_id")
    })
    it('Grab all sensor ids', () => {
        ids = []
        grabIds("sensor_id", "sensor_id%20IS%20NOT%20NULL")
    })
    it('Test for all sensor ids', () => {
        testIds("sensor_id")
    })
    it('Grab niirs MinMax', () => {
        minMax = []
        grabMinMax("niirs", "niirs%20IS%20NOT%20NULL")
    })
    it('Test for several niirs ranges', () => {
        testRange("niirs")
    })
    it('Grab azimuth_angle MinMax', () => {
        minMax = []
        grabMinMax("azimuth_angle", "azimuth_angle%20IS%20NOT%20NULL")
    })
    it('Test for several azimuth_angle ranges', () => {
        testRange("azimuth_angle")
    })
    it('Grab grazing_angle MinMax', () => {
        minMax = []
        grabMinMax("grazing_angle", "grazing_angle%20IS%20NOT%20NULL")
    })
    it('Test for several grazing_angle ranges', () => {
        testRange("grazing_angle")
    })
    it('Grab sun_azimuth MinMax', () => {
        minMax = []
        grabMinMax("sun_azimuth", "sun_azimuth%20IS%20NOT%20NULL")
    })
    it('Test for several sun_azimuth ranges', () => {
        testRange("sun_azimuth")
    })
    it('Grab sun_elevation MinMax', () => {
        minMax = []
        grabMinMax("sun_elevation", "sun_elevation%20IS%20NOT%20NULL")
    })
    it('Test for several sun_elevation ranges', () => {
        testRange("sun_elevation")
    })
    it('Grab cloud_cover MinMax', () => {
        minMax = []
        grabMinMax("cloud_cover", "cloud_cover%20IS%20NOT%20NULL")
    })
    it('Test for several cloud_cover ranges', () => {
        testRange("cloud_cover")
    })
    it('Grab GSD MinMax', () => {
        minMax = []
        grabMinMax("gsdy", "gsdy%20IS%20NOT%20NULL")
    })
    it('Test for several GSD ranges', () => {
        testRange("gsdy")
    })
    it('Grab acquisition date MinMax', () => {
        minMax = []
        grabMinMax("acquisition_date", "acquisition_date%20IS%20NOT%20NULL")
    })
    it('Test for several acquisition date ranges', () => {
        testDate("acquisition_date")
    })
    it('Grab ingest date MinMax', () => {
        minMax = []
        grabMinMax("ingest_date", "ingest_date%20IS%20NOT%20NULL")
    })
    it('Test for several ingest date ranges', () => {
        testDate("ingest_date")
    })
    it('Test for some geospatial bounds', () => {
        let minLat, minLong, maxLat, maxLong
        features.forEach((feature) => {
            let temp = feature.geometry.coordinates[0][0]
            let tLeft = temp[0][0]
            let tBottom = temp[2][1]
            let tRight = temp[2][0]
            let tTop = temp[0][1]
            if(minLat == null) {
                minLat = tBottom
                minLong = tLeft
                maxLat = tTop
                maxLong = tRight
            }
            else {
                if(tBottom < minLat) {
                    minLat = tBottom
                }
                else if(tTop > maxLat) {
                    maxLat = tTop
                }
                if(tLeft < minLong) {
                    minLong = tLeft
                }
                else if(tRight > maxLong) {
                    maxLong = tRight
                }
            }
        })
        let latDif = (maxLat - minLat) / 3
        let longDif = (maxLong - minLong) / 4
        for(var x = minLat; x < maxLat; x += latDif) {
            for(var y = minLong; y < maxLong; y += longDif) {
                let left = y
                let bottom = x
                let right = (y+longDif)
                let top = (x+latDif)
                cy.request({method: "GET", url: "/wfs?filter=BBOX(ground_geom%2C"+left+"%2C"+bottom+"%2C"+right+"%2C"+top+")&outputFormat=JSON&request=GetFeature&service=WFS&sortBy=acquisition_date%2BD&startIndex=0&typeName=omar%3Araster_entry&version=1.1.0"})
                    .then((response) => {
                        let temp
                        for(var i = 0; i < response.body.totalFeatures; i++) {
                            temp = response.body.features[i].geometry.coordinates[0][0]
                            let tLeft = temp[0][0]
                            let tBottom = temp[2][1]
                            let tRight = temp[2][0]
                            let tTop = temp[0][1]
                            if(tLeft >= left && tLeft <= right) {
                                expect(tLeft).to.be.within(left, right)
                            }
                            else {
                                expect(tRight).to.be.within(left, right)
                            }
                            if(tBottom >= bottom && tBottom <= top) {
                                expect(tBottom).to.be.within(bottom, top)
                            }
                            else {
                                expect(tTop).to.be.within(bottom, top)
                            }
                        }
                    })
            }
        }
    })

    //Helper Functions
    function testRandom(property) {
        let names = []
        for(var i = 0; i < 4; i++) {
            names[i] = features[Math.floor(Math.random() * features.length)].properties[property]
        }
        names.forEach((name) => {
            cy.request({method: "GET", url: "/wfs?filter="+property+"%20LIKE%20%27%25"+name+"%25%27&outputFormat=JSON&request=GetFeature&service=WFS&sortBy=acquisition_date%2BD&startIndex=0&typeName=omar%3Araster_entry&version=1.1.0"})
                .then((response) => {
                    let temp
                    for(var i = 0; i < response.body.totalFeatures; i++) {
                        temp = response.body.features[i].properties[property]
                        expect(temp).to.eq(name)
                    }
                })
        })
    }
    function grabIds(property, request) {
        cy.request({method: "GET", url: "/wfs?filter=("+request+")&outputFormat=JSON&request=GetFeature&service=WFS&sortBy=acquisition_date%2BD&startIndex=0&typeName=omar%3Araster_entry&version=1.1.0"})
            .then((response) => {
                if(response.body.features == 0) {
                    return
                }
                else {
                    response.body.features.forEach((feature) => {
                        let temp = feature.properties[property]
                        if(!ids.includes(temp)) {
                            ids.push(temp)
                            request = request + "%20AND%20"+property+"%20NOT%20LIKE%20%27%25" + temp + "%25%27"
                        }
                    })
                    grabIds(property, request)
                }
        })
    }
    function testIds(property) {
        ids.forEach((id) => {
            cy.request({method: "GET", url: "/wfs?filter=("+property+"%20LIKE%20%27%25"+id+"%25%27)&outputFormat=JSON&request=GetFeature&service=WFS&sortBy=acquisition_date%2BD&startIndex=0&typeName=omar%3Araster_entry&version=1.1.0"})
                .then((response) => {
                    let temp
                    for(var i = 0; i < response.body.totalFeatures; i++) {
                        temp = response.body.features[i].properties[property]
                        expect(temp).to.contain(id)
                    }
                })
        })
    }
    function grabMinMax(property, request) {
        cy.request({method: "GET", url: "/wfs?filter=("+request+")&outputFormat=JSON&request=GetFeature&service=WFS&sortBy=acquisition_date%2BD&startIndex=0&typeName=omar%3Araster_entry&version=1.1.0"})
            .then((response) => {
                if(response.body.features == 0) {
                    return
                }
                else {
                    let min = minMax[0]
                    let max = minMax[1]
                    response.body.features.forEach((feature) => {
                        let n = feature.properties[property]
                        if(min == null) {
                            min = n
                            max = n
                        }
                        else {
                            if(n < min) {
                                min = n
                            }
                            else if(n > max) {
                                max = n
                            }
                        }
                    })
                    minMax = [min, max]
                    request = property + "%20IS%20NOT%20NULL%20AND%20("+property+"%20<%20%27"+min+"%27%20OR%20"+property+"%20>%20%27"+max+"%27)"
                    grabMinMax(property, request)
                }
        })
    }
    function testRange(property) {
        let min = minMax[0]
        let max = minMax[1]
        let n2 = parseInt(((max - min)/4) + min)
        let n3 = parseInt(((max - min)/4)*2 + min)
        let n4 = parseInt(((max - min)/4)*3 + min)
        cy.request({method: "GET", url: "/wfs?filter=("+property+"%20>=%20"+min+"%20AND%20"+property+"%20<=%20"+n2+")&outputFormat=JSON&request=GetFeature&service=WFS&sortBy=acquisition_date%2BD&startIndex=0&typeName=omar%3Araster_entry&version=1.1.0"})
            .then((response) => {
                let temp
                for(var j = 0; j < response.body.totalFeatures; j++) {
                    temp = response.body.features[j].properties[property]
                    expect(temp).to.be.within(min, n2)
                }
            })
        cy.request({method: "GET", url: "/wfs?filter=("+property+"%20>=%20"+n2+"%20AND%20"+property+"%20<=%20"+n3+")&outputFormat=JSON&request=GetFeature&service=WFS&sortBy=acquisition_date%2BD&startIndex=0&typeName=omar%3Araster_entry&version=1.1.0"})
            .then((response) => {
                let temp
                for(var j = 0; j < response.body.totalFeatures; j++) {
                    temp = response.body.features[j].properties[property]
                    expect(temp).to.be.within(n2, n3)
                }
            })
        cy.request({method: "GET", url: "/wfs?filter=("+property+"%20>=%20"+n3+"%20AND%20"+property+"%20<=%20"+n4+")&outputFormat=JSON&request=GetFeature&service=WFS&sortBy=acquisition_date%2BD&startIndex=0&typeName=omar%3Araster_entry&version=1.1.0"})
            .then((response) => {
                let temp
                for(var j = 0; j < response.body.totalFeatures; j++) {
                    temp = response.body.features[j].properties[property]
                    expect(temp).to.be.within(n3, n4)
                }
            })
        cy.request({method: "GET", url: "/wfs?filter=("+property+"%20>=%20"+n4+"%20AND%20"+property+"%20<=%20"+max+")&outputFormat=JSON&request=GetFeature&service=WFS&sortBy=acquisition_date%2BD&startIndex=0&typeName=omar%3Araster_entry&version=1.1.0"})
            .then((response) => {
                let temp
                for(var j = 0; j < response.body.totalFeatures; j++) {
                    temp = response.body.features[j].properties[property]
                    expect(temp).to.be.within(n4, max)
                }
            })
    }
    function testDate(property) {
        let min = minMax[0]
        let max = minMax[1]
        let mnyear = min.substring(0,4)
        let mnmonth = min.substring(5,7) - 1
        let mnday = min.substring(8, 10)
        let mnhour = min.substring(11, 13)
        let mnminute = min.substring(14, 16)
        let mnsecond = min.substring(17, 19)
        let minDate = new Date(Date.UTC(mnyear, mnmonth, mnday, mnhour, mnminute, mnsecond))
        let mxyear = max.substring(0,4)
        let mxmonth = max.substring(5,7) - 1
        let mxday = max.substring(8, 10)
        let mxhour = max.substring(11, 13)
        let mxminute = max.substring(14, 16)
        let mxsecond = max.substring(17, 19)
        let maxDate = new Date(Date.UTC(mxyear, mxmonth, mxday, mxhour, mxminute, mxsecond))
        let date2 = new Date(((maxDate - minDate) / 4) + minDate.getTime())
        let date3 = new Date(((maxDate - minDate) / 4)*2 + minDate.getTime())
        let date4 = new Date(((maxDate - minDate) / 4)*3 + minDate.getTime())
        let start = min
        let end = (date2.getUTCMonth()+1) + "-" + date2.getUTCDate() + "-" + date2.getUTCFullYear()
        end = end + " " + date2.getUTCHours() + ":" + date2.getUTCMinutes() + ":" + date2.getUTCSeconds()
        cy.request({method: "GET", url: "/wfs?filter="+property+"%20%3E%3D%20%27"+start+"%2B0000%27%20AND%20"+property+"%20%3C%3D%20%27"+end+"%2B0000%27&outputFormat=JSON&request=GetFeature&service=WFS&sortBy=acquisition_date%2BD&startIndex=0&typeName=omar%3Araster_entry&version=1.1.0"})
            .then((response) => {
                let temp
                for(var i = 0; i < response.body.totalFeatures; i++) {
                    temp = response.body.features[i].properties[property]
                    let year = temp.substring(0,4)
                    let month = temp.substring(5,7) - 1
                    let day = temp.substring(8, 10)
                    let hour = temp.substring(11, 13)
                    let minute = temp.substring(14, 16)
                    let second = temp.substring(17, 19)
                    let date = new Date(Date.UTC(year, month, day, hour, minute, second))
                    expect(date).to.be.within(minDate, date2)
                }
            })
        start = end
        end = (date3.getUTCMonth()+1) + "-" + date3.getUTCDate() + "-" + date3.getUTCFullYear()
        end = end + " " + date3.getUTCHours() + ":" + date3.getUTCMinutes() + ":" + date3.getUTCSeconds()
        cy.request({method: "GET", url: "/wfs?filter="+property+"%20%3E%3D%20%27"+start+"%2B0000%27%20AND%20"+property+"%20%3C%3D%20%27"+end+"%2B0000%27&outputFormat=JSON&request=GetFeature&service=WFS&sortBy=acquisition_date%2BD&startIndex=0&typeName=omar%3Araster_entry&version=1.1.0"})
            .then((response) => {
                let temp
                for(var i = 0; i < response.body.totalFeatures; i++) {
                    temp = response.body.features[i].properties[property]
                    let year = temp.substring(0,4)
                    let month = temp.substring(5,7) - 1
                    let day = temp.substring(8, 10)
                    let hour = temp.substring(11, 13)
                    let minute = temp.substring(14, 16)
                    let second = temp.substring(17, 19)
                    let date = new Date(Date.UTC(year, month, day, hour, minute, second))
                    expect(date).to.be.within(date2, date3)
                }
            })
        start = end
        end = (date4.getUTCMonth()+1) + "-" + date4.getUTCDate() + "-" + date4.getUTCFullYear()
        end = end + " " + date4.getUTCHours() + ":" + date4.getUTCMinutes() + ":" + date4.getUTCSeconds()
        cy.request({method: "GET", url: "/wfs?filter="+property+"%20%3E%3D%20%27"+start+"%2B0000%27%20AND%20"+property+"%20%3C%3D%20%27"+end+"%2B0000%27&outputFormat=JSON&request=GetFeature&service=WFS&sortBy=acquisition_date%2BD&startIndex=0&typeName=omar%3Araster_entry&version=1.1.0"})
            .then((response) => {
                let temp
                for(var i = 0; i < response.body.totalFeatures; i++) {
                    temp = response.body.features[i].properties[property]
                    let year = temp.substring(0,4)
                    let month = temp.substring(5,7) - 1
                    let day = temp.substring(8, 10)
                    let hour = temp.substring(11, 13)
                    let minute = temp.substring(14, 16)
                    let second = temp.substring(17, 19)
                    let date = new Date(Date.UTC(year, month, day, hour, minute, second))
                    expect(date).to.be.within(date3, date4)
                }
            })
        start = end
        end = max
        cy.request({method: "GET", url: "/wfs?filter="+property+"%20%3E%3D%20%27"+start+"%2B0000%27%20AND%20"+property+"%20%3C%3D%20%27"+end+"%2B0000%27&outputFormat=JSON&request=GetFeature&service=WFS&sortBy=acquisition_date%2BD&startIndex=0&typeName=omar%3Araster_entry&version=1.1.0"})
            .then((response) => {
                let temp
                for(var i = 0; i < response.body.totalFeatures; i++) {
                    temp = response.body.features[i].properties[property]
                    let year = temp.substring(0,4)
                    let month = temp.substring(5,7) - 1
                    let day = temp.substring(8, 10)
                    let hour = temp.substring(11, 13)
                    let minute = temp.substring(14, 16)
                    let second = temp.substring(17, 19)
                    let date = new Date(Date.UTC(year, month, day, hour, minute, second))
                    expect(date).to.be.within(date4, maxDate)
                }
            })
    }
})