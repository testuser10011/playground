CREATE TABLE IF NOT EXISTS companiesCatalogue
(
    id          INTEGER PRIMARY KEY,
    companyName STRING UNIQUE NOT NULL
);


CREATE TABLE IF NOT EXISTS company
(
    id        INTEGER PRIMARY KEY ASC AUTOINCREMENT
                      NOT NULL,
    companyID INTEGER NOT NULL
        REFERENCES companiesCatalogue (id),
    date      DATE,
    value     DOUBLE  NOT NULL
);

CREATE TABLE IF NOT EXISTS portfolio
(
    id               INTEGER PRIMARY KEY NOT NULL,
    numStocksCurrent BIGINT DEFAULT (0),
    companyID        INTEGER             NOT NULL
        REFERENCES companiesCatalogue (id)
        UNIQUE ON CONFLICT IGNORE,
    numStocksStart   BIGINT DEFAULT (0)
);

CREATE TABLE IF NOT EXISTS transactions
(
    id        INTEGER PRIMARY KEY AUTOINCREMENT,
    companyID INTEGER REFERENCES company (id)
                       NOT NULL,
    quantity  BIGINT DEFAULT (0),
    transact  STRING   NOT NULL,
    date      DATETIME NOT NULL
);

