import { FormControl, FormGroup } from '@angular/forms';

import {
  dateRangeValidator,
  distinctAccountsValidator,
  minimumAgeValidator,
  positiveAmountValidator,
} from './financial-validators';

describe('financial validators', () => {
  it('rejects a customer younger than 18', () => {
    const control = new FormControl('2008-06-16');
    const validator = minimumAgeValidator(18, () => new Date(2026, 5, 15));

    expect(validator(control)).toEqual({ minimumAge: { requiredAge: 18 } });
  });

  it('accepts a customer on their 18th birthday', () => {
    const control = new FormControl('2008-06-15');
    const validator = minimumAgeValidator(18, () => new Date(2026, 5, 15));

    expect(validator(control)).toBeNull();
  });

  it('rejects a statement start date after the end date', () => {
    const form = new FormGroup({
      startDate: new FormControl('2026-06-16T10:00'),
      endDate: new FormControl('2026-06-15T10:00'),
    });

    expect(dateRangeValidator(form)).toEqual({ invalidDateRange: true });
  });

  it('accepts a valid statement date range', () => {
    const form = new FormGroup({
      startDate: new FormControl('2026-06-14T10:00'),
      endDate: new FormControl('2026-06-15T10:00'),
    });

    expect(dateRangeValidator(form)).toBeNull();
  });

  it('rejects zero and negative transaction amounts', () => {
    expect(positiveAmountValidator(new FormControl(0))).toEqual({ positiveAmount: true });
    expect(positiveAmountValidator(new FormControl(-100))).toEqual({ positiveAmount: true });
  });

  it('rejects a transfer to the source account', () => {
    const form = new FormGroup({
      sourceAccountNumber: new FormControl('1000000001'),
      destinationAccountNumber: new FormControl('1000000001'),
    });

    expect(distinctAccountsValidator(form)).toEqual({ sameAccount: true });
  });
});
