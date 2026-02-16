import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Router } from '@angular/router';
import { of, EMPTY, Observable } from 'rxjs';
import { mergeMap } from 'rxjs/operators';

import { IEntitedetest } from '../entitedetest.model';
import { EntitedetestService } from '../service/entitedetest.service';

const entitedetestResolve = (route: ActivatedRouteSnapshot): Observable<null | IEntitedetest> => {
  const id = route.params['id'];
  if (id) {
    return inject(EntitedetestService)
      .find(id)
      .pipe(
        mergeMap((entitedetest: HttpResponse<IEntitedetest>) => {
          if (entitedetest.body) {
            return of(entitedetest.body);
          } else {
            inject(Router).navigate(['404']);
            return EMPTY;
          }
        }),
      );
  }
  return of(null);
};

export default entitedetestResolve;
