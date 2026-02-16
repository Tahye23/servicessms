import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Router } from '@angular/router';
import { of, EMPTY, Observable } from 'rxjs';
import { mergeMap } from 'rxjs/operators';

import { IPlanabonnement } from '../planabonnement.model';
import { PlanabonnementService } from '../service/planabonnement.service';

const planabonnementResolve = (route: ActivatedRouteSnapshot): Observable<null | IPlanabonnement> => {
  const id = route.params['id'];
  if (id) {
    return inject(PlanabonnementService)
      .find(id)
      .pipe(
        mergeMap((planabonnement: HttpResponse<IPlanabonnement>) => {
          if (planabonnement.body) {
            return of(planabonnement.body);
          } else {
            inject(Router).navigate(['404']);
            return EMPTY;
          }
        }),
      );
  }
  return of(null);
};

export default planabonnementResolve;
