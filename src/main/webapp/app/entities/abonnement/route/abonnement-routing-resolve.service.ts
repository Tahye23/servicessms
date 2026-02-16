import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Router } from '@angular/router';
import { of, EMPTY, Observable } from 'rxjs';
import { mergeMap } from 'rxjs/operators';

import { IAbonnement } from '../abonnement.model';
import { AbonnementService } from '../service/abonnement.service';

const abonnementResolve = (route: ActivatedRouteSnapshot): Observable<null | IAbonnement> => {
  const id = route.params['id'];
  if (id) {
    return inject(AbonnementService)
      .find(id)
      .pipe(
        mergeMap((abonnement: HttpResponse<IAbonnement>) => {
          if (abonnement.body) {
            return of(abonnement.body);
          } else {
            inject(Router).navigate(['404']);
            return EMPTY;
          }
        }),
      );
  }
  return of(null);
};

export default abonnementResolve;
