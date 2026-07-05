alter table products add column sort_order integer;

update products p
set sort_order = ((select max(id) from products) - p.id + 1) * 10;

alter table products alter column sort_order set default 0;
alter table products alter column sort_order set not null;
