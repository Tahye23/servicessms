import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Router } from '@angular/router';
import { of, EMPTY, Observable } from 'rxjs';
import { mergeMap } from 'rxjs/operators';

import { IReponse } from '../reponse.model';
import { ReponseService } from '../service/reponse.service';

const reponseResolve = (route: ActivatedRouteSnapshot): Observable<null | IReponse> => {
  const id = route.params['id'];
  if (id) {
    return inject(ReponseService)
      .find(id)
      .pipe(
        mergeMap((reponse: HttpResponse<IReponse>) => {
          if (reponse.body) {
            return of(reponse.body);
          } else {
            inject(Router).navigate(['404']);
            return EMPTY;
          }
        }),
      );
  }
  return of(null);
};

export default reponseResolve;
