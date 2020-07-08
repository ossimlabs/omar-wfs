describe('My First Test', () => {
    it('clicking "type" navigates to a new url', () => {
        cy.visit('https://omar.ossim.io/omar-wfs/webjars/swagger-ui/3.24.0/index.html?url=/omar-wfs/apidoc/getDocuments')

        cy.contains('getFeature').click()
        cy.url().should('include', '/default/getFeature')
        cy.contains('Try it out').click()
        cy.contains('Execute').click()
        cy.contains('200').should('include.text', '200')
    })
})