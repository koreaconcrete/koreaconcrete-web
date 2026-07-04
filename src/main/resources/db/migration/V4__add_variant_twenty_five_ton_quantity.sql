alter table product_variants
    add column twenty_five_ton_quantity numeric(12, 2);

update product_variants
set twenty_five_ton_quantity = floor(25000 / weight_kg)
where weight_kg is not null
  and weight_kg > 0;
