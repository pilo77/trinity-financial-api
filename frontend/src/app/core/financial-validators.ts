import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

const STRICT_EMAIL_PATTERN = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;

export const strictEmailValidator: ValidatorFn = (
  control: AbstractControl,
): ValidationErrors | null => {
  if (!control.value) return null;

  return STRICT_EMAIL_PATTERN.test(String(control.value)) ? null : { strictEmail: true };
};

export const minimumAgeValidator = (
  minimumAge: number,
  todayProvider: () => Date = () => new Date(),
): ValidatorFn => {
  return (control: AbstractControl): ValidationErrors | null => {
    if (!control.value) return null;

    const birthDate = new Date(`${control.value}T00:00:00`);
    if (Number.isNaN(birthDate.getTime())) return { invalidDate: true };

    const today = todayProvider();
    const minimumBirthDate = new Date(
      today.getFullYear() - minimumAge,
      today.getMonth(),
      today.getDate(),
    );

    return birthDate <= minimumBirthDate ? null : { minimumAge: { requiredAge: minimumAge } };
  };
};

export const dateRangeValidator: ValidatorFn = (
  control: AbstractControl,
): ValidationErrors | null => {
  const startDate = control.get('startDate')?.value;
  const endDate = control.get('endDate')?.value;
  if (!startDate || !endDate) return null;

  return new Date(startDate).getTime() <= new Date(endDate).getTime()
    ? null
    : { invalidDateRange: true };
};

export const distinctAccountsValidator: ValidatorFn = (
  control: AbstractControl,
): ValidationErrors | null => {
  const source = control.get('sourceAccountNumber')?.value;
  const destination = control.get('destinationAccountNumber')?.value;
  if (!source || !destination) return null;

  return source === destination ? { sameAccount: true } : null;
};

export const positiveAmountValidator: ValidatorFn = (
  control: AbstractControl,
): ValidationErrors | null => {
  const amount = Number(control.value);
  return Number.isFinite(amount) && amount > 0 ? null : { positiveAmount: true };
};
