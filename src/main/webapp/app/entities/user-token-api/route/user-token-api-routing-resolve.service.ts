import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Router } from '@angular/router';
import { of, EMPTY, Observable } from 'rxjs';
import { mergeMap } from 'rxjs/operators';

import { IUserTokenApi } from '../user-token-api.model';
import { UserTokenApiService } from '../service/user-token-api.service';

const userTokenApiResolve = (route: ActivatedRouteSnapshot): Observable<null | IUserTokenApi> => {
  const id = route.params['id'];
  if (id) {
    return inject(UserTokenApiService)
      .find(id)
      .pipe(
        mergeMap((userTokenApi: HttpResponse<IUserTokenApi>) => {
          if (userTokenApi.body) {
            return of(userTokenApi.body);
          } else {
            inject(Router).navigate(['404']);
            return EMPTY;
          }
        }),
      );
  }
  return of(null);
};

export default userTokenApiResolve;
