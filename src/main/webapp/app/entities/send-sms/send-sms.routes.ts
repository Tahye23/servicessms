import { Routes } from '@angular/router';

import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { ASC } from 'app/config/navigation.constants';
import { SendSmsComponent } from './list/send-sms.component';
import { SendSmsDetailComponent } from './detail/send-sms-detail.component';
import { SendSmsUpdateComponent } from './update/send-sms-update.component';
import SendSmsResolve from './route/send-sms-routing-resolve.service';
import { SmsDetailComponent } from './SmsDetail/sms_detail.component';

const sendSmsRoute: Routes = [
  {
    path: '',
    component: SendSmsComponent,
    data: {
      defaultSort: 'id,' + ASC,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/view',
    component: SendSmsDetailComponent,
    resolve: {
      sendSms: SendSmsResolve,
    },
    canActivate: [UserRouteAccessService],
  },

  {
    path: 'new',
    component: SendSmsUpdateComponent,
    resolve: {
      sendSms: SendSmsResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/edit',
    component: SendSmsUpdateComponent,
    resolve: {
      sendSms: SendSmsResolve,
    },
    canActivate: [UserRouteAccessService],
  },
];

export default sendSmsRoute;
