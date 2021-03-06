/*! SET storage_engine=INNODB */;

DROP TABLE IF EXISTS invoice_items;
CREATE TABLE invoice_items (
    record_id int(11) unsigned NOT NULL AUTO_INCREMENT,
    id char(36) NOT NULL,
    type varchar(24) NOT NULL,
    invoice_id char(36) NOT NULL,
    account_id char(36) NOT NULL,
    bundle_id char(36),
    subscription_id char(36),
    plan_name varchar(50),
    phase_name varchar(50),
    start_date date NOT NULL,
    end_date date,
    amount numeric(10,4) NOT NULL,
    rate numeric(10,4) NULL,
    currency char(3) NOT NULL,
    linked_item_id char(36),
    created_by varchar(50) NOT NULL,
    created_date datetime NOT NULL,
    account_record_id int(11) unsigned default null,
    tenant_record_id int(11) unsigned default null,
    PRIMARY KEY(record_id)
);
CREATE UNIQUE INDEX invoice_items_id ON invoice_items(id);
CREATE INDEX invoice_items_subscription_id ON invoice_items(subscription_id ASC);
CREATE INDEX invoice_items_invoice_id ON invoice_items(invoice_id ASC);
CREATE INDEX invoice_items_account_id ON invoice_items(account_id ASC);
CREATE INDEX invoice_items_tenant_account_record_id ON invoice_items(tenant_record_id, account_record_id);

DROP TABLE IF EXISTS invoices;
CREATE TABLE invoices (
    record_id int(11) unsigned NOT NULL AUTO_INCREMENT,
    id char(36) NOT NULL,
    account_id char(36) NOT NULL,
    invoice_date date NOT NULL,
    target_date date NOT NULL,
    currency char(3) NOT NULL,
    migrated bool NOT NULL,
    created_by varchar(50) NOT NULL,
    created_date datetime NOT NULL,
    account_record_id int(11) unsigned default null,
    tenant_record_id int(11) unsigned default null,
    PRIMARY KEY(record_id)
);
CREATE UNIQUE INDEX invoices_id ON invoices(id);
CREATE INDEX invoices_account_target ON invoices(account_id ASC, target_date);
CREATE INDEX invoices_tenant_account_record_id ON invoices(tenant_record_id, account_record_id);

DROP TABLE IF EXISTS invoice_payments;
CREATE TABLE invoice_payments (
    record_id int(11) unsigned NOT NULL AUTO_INCREMENT,
    id char(36) NOT NULL,
    type varchar(24) NOT NULL,    
    invoice_id char(36) NOT NULL,
    payment_id char(36),
    payment_date datetime NOT NULL,
    amount numeric(10,4) NOT NULL,
    currency char(3) NOT NULL,
    payment_cookie_id char(36) DEFAULT NULL,    
    linked_invoice_payment_id char(36) DEFAULT NULL,
    created_by varchar(50) NOT NULL,
    created_date datetime NOT NULL,
    account_record_id int(11) unsigned default null,
    tenant_record_id int(11) unsigned default null,
    PRIMARY KEY(record_id)
);
CREATE UNIQUE INDEX invoice_payments_id ON invoice_payments(id);
CREATE INDEX invoice_payments ON invoice_payments(payment_id);
CREATE INDEX invoice_payments_invoice_id ON invoice_payments(invoice_id);
CREATE INDEX invoice_payments_reversals ON invoice_payments(linked_invoice_payment_id);
CREATE INDEX invoice_payments_tenant_account_record_id ON invoice_payments(tenant_record_id, account_record_id);
