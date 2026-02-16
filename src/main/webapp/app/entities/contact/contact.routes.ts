import { Routes } from '@angular/router';

import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { ASC } from 'app/config/navigation.constants';
import { ContactComponent } from './list/contact.component';
import { ContactDetailComponent } from './detail/contact-detail.component';
import { ContactUpdateComponent } from './update/contact-update.component';
import ContactResolve from './route/contact-routing-resolve.service';

const contactRoute: Routes = [
  {
    path: '',
    component: ContactComponent,
    data: {
      defaultSort: 'id,' + ASC,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/view',
    component: ContactDetailComponent,
    resolve: {
      contact: ContactResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'new',
    component: ContactUpdateComponent,
    resolve: {
      contact: ContactResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/edit',
    component: ContactUpdateComponent,
    resolve: {
      contact: ContactResolve,
    },
    canActivate: [UserRouteAccessService],
  },
];

export default contactRoute;
