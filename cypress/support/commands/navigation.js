import { RewriteURL } from '../utils/constants';
import { humanReadableNow } from '../../support/utils/utils';
import { checkIfWindowCanExecuteActions } from './commands_utils';
/*
 * Basic command for clicking a button element having a certain text
 * @param text string to search for in the button
 */
Cypress.Commands.add('clickButtonWithText', text => {
  cy.get('button')
    .contains(text)
    .should('exist')
    .click();
});

/*
 * Basic command for clicking an element with certain selector
 * @param selector string used to query for the element
 */
Cypress.Commands.add('clickElementWithClass', (selector, force) => {
  const opts = {};

  if (force) {
    opts.force = true;
  }

  cy.get(selector)
    .should('exist')
    .click({ ...opts });
});

Cypress.Commands.add('selectTab', (tabName, force) => {
  const opts = {};

  if (force) {
    opts.force = true;
  }

  return cy.get(`#tab_${tabName}`).click(opts);
});

Cypress.Commands.add('selectSingleTabRow', () => {
  cy.get('.table-flex-wrapper')
    .find('tbody tr')
    .should('exist')
    .click({ force: true });
});

Cypress.Commands.add('openReferencedDocuments', (referenceId, retriesLeft = 8) => {
  // retry 8 times to open the referenced document
  const date = humanReadableNow();
  const timeout = { timeout: 20000 };
  checkIfWindowCanExecuteActions();

  if (retriesLeft >= 1) {
    const referencesAliasName = `references-${date}`;
    cy.server();
    cy.route('GET', new RegExp(RewriteURL.REFERENCES)).as(referencesAliasName);

    cy.get('body').type('{alt}6'); // open referenced docs
    cy.wait(`@${referencesAliasName}`, timeout);
    cy.get('.order-list-panel .order-list-loader', timeout).should('not.exist');

    return cy.get('body').then(body => {
      if (body.find(`.reference_${referenceId}`).length > 0) {
        return cy.get(`.reference_${referenceId}`).click();
      } else {
        cy.wait(1000);
        cy.get('body').type('{alt}5'); // close referenced docs by switching to something else
        return cy.openReferencedDocuments(referenceId, retriesLeft - 1);
      }
    });
  }
  cy.get('body').type('{alt}6'); // open referenced docs
  return cy.get(`.reference_${referenceId}`).click(); // one more time just because we need to throw the error
});

/**
 * Select the nth row in a list. Starts from 0.
 *
 * @param rowNumber - the row number
 */
Cypress.Commands.add('selectNthRow', (rowNumber, modal = false, force = false) => {
  let path = '.table-flex-wrapper';

  if (modal) {
    path = '.modal-content-wrapper ' + path;
  }

  return cy
    .get(path)
    .find(`tbody tr:nth-child(${rowNumber + 1})`)
    .should('exist')
    .click()
    .then(el => {
      if (!force) {
        cy.waitForSaveIndicator();
      }
      return cy.wrap(el);
    });
});

/**
 * Expect the table to have a specific number of rows
 *
 * @param numberOfRows - the number of rows
 */
Cypress.Commands.add('expectNumberOfRows', numberOfRows => {
  return cy.get('table tbody tr').should('have.length', numberOfRows);
});

/**
 * Expect the table rows to be greater than a given number
 *
 * @param numberOfRows - the number of rows
 */
Cypress.Commands.add('expectNumberOfRowsToBeGreaterThan', numberOfRows => {
  return cy.get('table tbody tr').should(el => {
    expect(el).to.have.length.greaterThan(numberOfRows);
  });
});

Cypress.Commands.add('selectRowByColumnAndValue', (columnName, expectedValue, modal = false, force = false) => {
  cy.log(`Select row by columnName=${columnName} and expectedValue=${expectedValue}`);
  if (!force) {
    cy.waitForSaveIndicator();
  }
  let path = '.table-flex-wrapper';

  if (modal) {
    path = '.modal-content-wrapper ' + path;
  }

  // step 1: figure out from the thead what is the column index
  return cy
    .get(path)
    .contains('thead tr th', columnName)
    .invoke('index')
    .then(columnIndex => {
      // step 2: take the cell which contains the expectedValue in columnNumber from above
      return cy
        .get(path)
        .contains(`tbody td:nth-child(${columnIndex + 1})`, expectedValue)
        .should('exist')
        .click()
        .then(td => {
          // step 3: return the row containing that cell
          const tr = td.parent();
          return cy.wrap(tr).should('have.class', 'row-selected');
        });
    });
});
/**
 * Select all rows on the current page
 *
 */
Cypress.Commands.add('selectAllRowsOnCurrentPage', () => {
  cy.get('body').type('{alt}a');
});
/**
 * Select all rows from all pages
 * This function only works on a list window, and not on a single view window
 */
Cypress.Commands.add('countAllRows', () => {
  cy.get('.pagination-row .pagination-part .hidden-sm-down').then(totalString => {
    const totalRows = parseInt(
      totalString
        .text()
        .split(' ')
        .pop(),
      10
    );
    cy.log(`Total number of rows on all pages is: ${totalRows}`);
    return cy.wrap(totalRows);
  });
});
