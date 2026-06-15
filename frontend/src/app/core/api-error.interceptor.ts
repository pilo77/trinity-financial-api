import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';

import { ProblemDetail } from './models';

export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
    readonly code?: string,
  ) {
    super(message);
  }
}

export const apiErrorInterceptor: HttpInterceptorFn = (request, next) =>
  next(request).pipe(
    catchError((error: HttpErrorResponse) => {
      const problem = error.error as ProblemDetail | undefined;
      const fieldMessage = problem?.fieldErrors?.find((item) => item.message)?.message;
      const message =
        fieldMessage ??
        problem?.detail ??
        (error.status === 0
          ? 'No fue posible conectar con la API. Verifica que el backend este disponible.'
          : 'La operacion no pudo completarse.');

      return throwError(() => new ApiError(message, error.status, problem?.code));
    }),
  );
