SELECT
    p1_0.id,
    p1_0.created_at,
    p1_0.deleted_at,
    p1_0.name,
    p1_0.price,
    p1_0.stock,
    p1_0.updated_at,
    SUM(oie1_0.quantity) AS sold_count
FROM
    order_item oie1_0
JOIN
    product p1_0
        ON p1_0.id = oie1_0.product_id
        AND (p1_0.deleted_at IS NULL)
WHERE
    oie1_0.deleted_at IS NULL
    AND oie1_0.created_at BETWEEN :searchFrom AND :searchUntil
GROUP BY
    p1_0.id
ORDER BY
    sold_count DESC
LIMIT :offset, :limit ;
