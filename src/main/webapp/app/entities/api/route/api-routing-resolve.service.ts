import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Router } from '@angular/router';
import { of, EMPTY, Observable } from 'rxjs';
import { mergeMap } from 'rxjs/operators';

import { IApi } from '../api.model';
import { ApiService } from '../service/api.service';

const apiResolve = (route: ActivatedRouteSnapshot): Observable<null | IApi> => {
  const id = route.params['id'];
  if (id) {
    return inject(ApiService)
      .find(id)
      .pipe(
        mergeMap((api: HttpResponse<IApi>) => {
          if (api.body) {
            return of(api.body);
          } else {
            inject(Router).navigate(['404']);
            return EMPTY;
          }
        }),
      );
  }
  return of(null);
};

export default apiResolve;
