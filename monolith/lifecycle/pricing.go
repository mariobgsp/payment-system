package lifecycle

import "errors"

// calcPriceCharge replicates OrderUsecase discount logic but fixes bug.
// Java did: long discount = (long)(product.getDiscount()*100); price - price*discount/100
// That truncates 0.3→30, works but loses precision and ignores enable flag correctly.
// ponytail: integer math with float discount; switch to fixed-point cents if fractional drift matters.
func calcPriceCharge(price int64, discount float64, amount int64, enable bool) (int64, error) {
	if price <= 0 {
		return 0, errors.New("invalid price")
	}
	if amount <= 0 {
		return 0, errors.New("invalid amount")
	}
	// discount not available but caller requested enable → reject, mirrors OrderUsecase 07
	if enable && (discount <= 0 || discount >= 1) {
		// discount 0 or >=100% invalid when enable requested
		return 0, errors.New("discount not available, turn off enable discount flag")
	}
	unit := price
	if enable && discount > 0 {
		// apply discount: price*(1-discount)
		unit = int64(float64(price) * (1 - discount))
		if unit <= 0 {
			unit = 1
		}
	}
	return unit * amount, nil
}
