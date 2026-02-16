import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Router } from '@angular/router';
import { of, EMPTY, Observable } from 'rxjs';
import { mergeMap } from 'rxjs/operators';

import { IUserService } from '../user-service.model';
import { UserServiceService } from '../service/user-service.service';

const userServiceResolve = (route: ActivatedRouteSnapshot): Observable<null | IUserService> => {
  const id = route.params['id'];
  if (id) {
    return inject(UserServiceService)
      .find(id)
      .pipe(
        mergeMap((userService: HttpResponse<IUserService>) => {
          if (userService.body) {
            return of(userService.body);
          } else {
            inject(Router).navigate(['404']);
            return EMPTY;
          }
        }),
      );
  }
  return of(null);
};

export default userServiceResolve;
