import { Route } from '@angular/router';
import RequestComponent from './request.component';

const requestRoute: Route = {
  path: 'request/:plan', // <--- paramètre ici !
  component: RequestComponent,
  title: 'register.title',
};

export default requestRoute;
