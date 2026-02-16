import { Routes } from '@angular/router';
import { ASC } from 'app/config/navigation.constants';
import { CreateTemplateComponent } from './create-template/create-template.component';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { SendWhatsappComponent } from './list/send-whatsapp.component';
import { CreateCampagneComponent } from './create-campagne/create-campagne.component';
import { SendWhatsappDetailComponent } from './detail/send-whatsapp-detail.component';
import sendSmsResolve from '../send-sms/route/send-sms-routing-resolve.service';
import { SendWhatsappUpdateComponent } from './update/send-whatsapp-update.component';

const sendWhatsappRoute: Routes = [
  {
    path: '',
    component: SendWhatsappComponent,
    data: {},
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'new-template',
    component: CreateTemplateComponent,
    data: {},
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'new-campagne',
    component: CreateCampagneComponent,
    data: {},
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/view',
    component: SendWhatsappDetailComponent,
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/edit',
    component: SendWhatsappUpdateComponent,
    canActivate: [UserRouteAccessService],
  },
];

export default sendWhatsappRoute;
