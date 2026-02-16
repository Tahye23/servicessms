import { Pipe, PipeTransform } from '@angular/core';

import dayjs, { Dayjs } from 'dayjs/esm';

@Pipe({
  standalone: true,
  name: 'formatMediumDatetime',
})
export default class FormatMediumDatetimePipe implements PipeTransform {
  // transform(day: dayjs.Dayjs | null | undefined): string {
  //   return day ? day.format('D MMM YYYY HH:mm:ss') : '';
  // }

  transform(value: string | Date | Dayjs | null | undefined): string {
    if (!value) return '';

    const date = dayjs(value); // ici on parse la date
    return date.isValid() ? date.format('D MMM YYYY HH:mm:ss') : '';
  }
}
