let methods = ['getFeature', 'describeFeatureType', 'getCapabilities']

describe('Automated tests for the omar-wfs methods', () => {
    before(() => {
        function readTextFile(file, callback) {
            const rawFile = new XMLHttpRequest();
            rawFile.overrideMimeType("application/json");
            rawFile.open("GET", file, true);
            rawFile.onreadystatechange = function() {
                if (rawFile.readyState === 4 && rawFile.status == "200") {
                    callback(rawFile.responseText);
                }
            }
            rawFile.send(null);
        }
        readTextFile("/home/IdeaProjects/omar-wfs/plugins/omar-wfs-plugin/build/swaggerSpec.json", function(text){
            methods = JSON.parse(text).keys;
        });
    })
    methods.forEach((method) => {
        it(`Should test ${method}`, () => {
            cy.visit('https://omar.ossim.io/omar-wfs/webjars/swagger-ui/3.24.0/index.html?url=/omar-wfs/apidoc/getDocuments')
            cy.contains(method).should('be.visible').click()
            cy.url().should('include', '/default/' + method)
            cy.contains('Try it out').click()
            cy.contains('Execute').click()
            cy.contains('200').should('include.text', '200')
        })
    })
})