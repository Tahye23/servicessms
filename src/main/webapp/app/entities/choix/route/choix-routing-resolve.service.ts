import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Router } from '@angular/router';
import { of, EMPTY, Observable } from 'rxjs';
import { mergeMap } from 'rxjs/operators';

import { IChoix } from '../choix.model';
import { ChoixService } from '../service/choix.service';

const choixResolve = (route: ActivatedRouteSnapshot): Observable<null | IChoix> => {
  const id = route.params['id'];
  if (id) {
    return inject(ChoixService)
      .find(id)
      .pipe(
        mergeMap((choix: HttpResponse<IChoix>) => {
          if (choix.body) {
            return of(choix.body);
          } else {
            inject(Router).navigate(['404']);
            return EMPTY;
          }
        }),
      );
  }
  return of(null);
};

export default choixResolve;
