@echo off
setlocal

REM MySQL connection (edit if needed)
set HOST=127.0.0.1
set PORT=3306
set USER=root

echo Seeding users...
mysql -h %HOST% -P %PORT% -u %USER% -p < docs/issue-#18/users.bulk-insert.sql

echo Seeding products...
mysql -h %HOST% -P %PORT% -u %USER% -p < docs/issue-#18/products.bulk-insert.sql

echo Seeding coupons...
mysql -h %HOST% -P %PORT% -u %USER% -p < docs/issue-#18/coupon.insert.sql

echo Seeding small orders...
mysql -h %HOST% -P %PORT% -u %USER% -p < docs/issue-#18/orders.bulk-insert.small.sql

echo Done.

endlocal

