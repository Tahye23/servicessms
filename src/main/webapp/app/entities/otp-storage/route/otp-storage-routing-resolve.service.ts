import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Router } from '@angular/router';
import { of, EMPTY, Observable } from 'rxjs';
import { mergeMap } from 'rxjs/operators';

import { IOTPStorage } from '../otp-storage.model';
import { OTPStorageService } from '../service/otp-storage.service';

const oTPStorageResolve = (route: ActivatedRouteSnapshot): Observable<null | IOTPStorage> => {
  const id = route.params['id'];
  if (id) {
    return inject(OTPStorageService)
      .find(id)
      .pipe(
        mergeMap((oTPStorage: HttpResponse<IOTPStorage>) => {
          if (oTPStorage.body) {
            return of(oTPStorage.body);
          } else {
            inject(Router).navigate(['404']);
            return EMPTY;
          }
        }),
      );
  }
  return of(null);
};

export default oTPStorageResolve;
