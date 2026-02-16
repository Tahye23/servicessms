import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Router } from '@angular/router';
import { of, EMPTY, Observable } from 'rxjs';
import { mergeMap } from 'rxjs/operators';

import { ISendSms } from '../send-sms.model';
import { SendSmsService } from '../service/send-sms.service';

const sendSmsResolve = (route: ActivatedRouteSnapshot): Observable<null | ISendSms> => {
  const id = route.params['id'];
  if (id) {
    return inject(SendSmsService)
      .find(id)
      .pipe(
        mergeMap((sendSms: HttpResponse<ISendSms>) => {
          if (sendSms.body) {
            return of(sendSms.body);
          } else {
            inject(Router).navigate(['404']);
            return EMPTY;
          }
        }),
      );
  }
  return of(null);
};

export default sendSmsResolve;
