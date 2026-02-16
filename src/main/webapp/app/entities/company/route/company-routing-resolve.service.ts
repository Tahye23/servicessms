import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Router } from '@angular/router';
import { of, EMPTY, Observable } from 'rxjs';
import { mergeMap } from 'rxjs/operators';

import { ICompany } from '../company.model';
import { CompanyService } from '../service/company.service';

const companyResolve = (route: ActivatedRouteSnapshot): Observable<null | ICompany> => {
  const id = route.params['id'];
  if (id) {
    return inject(CompanyService)
      .find(id)
      .pipe(
        mergeMap((company: HttpResponse<ICompany>) => {
          if (company.body) {
            return of(company.body);
          } else {
            inject(Router).navigate(['404']);
            return EMPTY;
          }
        }),
      );
  }
  return of(null);
};

export default companyResolve;
