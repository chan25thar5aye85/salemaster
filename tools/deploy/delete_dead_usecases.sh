#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/../.."
echo "▸ cwd: $(pwd)"
echo

DEAD=(
  app/src/main/java/com/akari/retailer/features/expense/domain/usecases/AddExpenseUseCase.kt
  app/src/main/java/com/akari/retailer/features/expense/domain/usecases/UpdateExpenseUseCase.kt
  app/src/main/java/com/akari/retailer/features/expense/domain/usecases/DeleteExpenseUseCase.kt
  app/src/main/java/com/akari/retailer/features/expense/domain/usecases/GetExpensesUseCase.kt
  app/src/main/java/com/akari/retailer/features/inventory/domain/usecases/AddProductUseCase.kt
  app/src/main/java/com/akari/retailer/features/inventory/domain/usecases/UpdateProductUseCase.kt
  app/src/main/java/com/akari/retailer/features/inventory/domain/usecases/DeleteProductUseCase.kt
  app/src/main/java/com/akari/retailer/features/inventory/domain/usecases/GetProductsUseCase.kt
  app/src/main/java/com/akari/retailer/features/inventory/domain/usecases/GetOrdersUseCase.kt
  app/src/main/java/com/akari/retailer/features/inventory/domain/usecases/AddStockUseCase.kt
  app/src/main/java/com/akari/retailer/features/inventory/domain/usecases/RemoveStockUseCase.kt
  app/src/main/java/com/akari/retailer/features/inventory/domain/usecases/AdjustStockUseCase.kt
  app/src/main/java/com/akari/retailer/features/customer/domain/usecases/AddCustomerUseCase.kt
  app/src/main/java/com/akari/retailer/features/customer/domain/usecases/DeleteCustomerUseCase.kt
  app/src/main/java/com/akari/retailer/features/customer/domain/usecases/GetCustomersUseCase.kt
  app/src/main/java/com/akari/retailer/features/supplier/domain/usecases/AddSupplierUseCase.kt
  app/src/main/java/com/akari/retailer/features/supplier/domain/usecases/UpdateSupplierUseCase.kt
  app/src/main/java/com/akari/retailer/features/supplier/domain/usecases/DeleteSupplierUseCase.kt
  app/src/main/java/com/akari/retailer/features/supplier/domain/usecases/GetSuppliersUseCase.kt
  app/src/main/java/com/akari/retailer/features/sales/domain/usecases/AddSaleUseCase.kt
  app/src/main/java/com/akari/retailer/features/sales/domain/usecases/GetSalesUseCase.kt
  app/src/main/java/com/akari/retailer/features/sales/domain/usecases/GetTodaySalesUseCase.kt
  app/src/main/java/com/akari/retailer/features/sales/domain/usecases/DeleteSaleUseCase.kt
)

DELETED=0
SKIPPED=0
MISSING=0

for f in "${DEAD[@]}"; do
  if [[ ! -f "$f" ]]; then
    echo "⏭  missing: $(basename "$f")"
    MISSING=$((MISSING+1))
    continue
  fi
  cls="$(basename "$f" .kt)"
  refs=$(grep -rl --include='*.kt' "\\b${cls}\\b" app/src/main/java | grep -v "^${f}$" || true)
  ref_count=$(printf '%s\n' "$refs" | grep -c . || true)

  if [[ "$ref_count" -gt 0 ]]; then
    echo "⚠  SKIP (referenced in $ref_count file(s)): $cls"
    printf '%s\n' "$refs" | sed 's/^/     → /'
    SKIPPED=$((SKIPPED+1))
    continue
  fi

  rm -f "$f"
  echo "🗑  removed $cls"
  DELETED=$((DELETED+1))
done

echo
echo "Deleted: $DELETED   Skipped: $SKIPPED   Missing: $MISSING"
