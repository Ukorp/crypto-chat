CREATE TABLE IF NOT EXISTS Padding
(
    value text not null
);

CREATE TABLE IF NOT EXISTS CipherMode
(
    value text not null
);

CREATE TABLE IF NOT EXISTS CipherName
(
    value text not null
);

insert into Padding
values ('ZEROS'),
       ('ANSIX923'),
       ('PKCS7'),
       ('ISO10126');

insert into CipherMode
values ('ECB'),
       ('CBC'),
       ('PCBC'),
       ('CFB'),
       ('CTR'),
       ('RD');

insert into CipherName
values ('MAGENTA');

CREATE TABLE IF NOT EXISTS sessions
(
    id          UUID PRIMARY KEY,
    sender      TEXT NOT NULL,
    recipient   TEXT NOT NULL,
    padding     TEXT NOT NULL CHECK (padding IN ('ZEROS', 'ANSIX923', 'PKCS7', 'ISO10126')),
    cipher_mode TEXT NOT NULL CHECK (cipher_mode IN ('ECB', 'CBC', 'PCBC', 'CFB', 'CTR', 'RD')),
    cipher_name TEXT NOT NULL CHECK (cipher_name IN ('MAGENTA', 'LOKI97')),
    iv          BLOB,
    key         BLOB NOT NULL
);