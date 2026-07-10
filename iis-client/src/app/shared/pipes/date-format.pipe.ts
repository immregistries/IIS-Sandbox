import {Pipe, PipeTransform} from '@angular/core';

@Pipe({name: 'iisDate', standalone: true})
export class DateFormatPipe implements PipeTransform {
  transform(value: string | null | undefined, format: 'short' | 'long' = 'short'): string {
    if (!value) return '';
    const date = new Date(value);
    if (isNaN(date.getTime())) return value;
    if (format === 'long') {
      return date.toLocaleDateString('en-US', {year: 'numeric', month: 'long', day: 'numeric'});
    }
    return date.toLocaleDateString('en-US', {year: 'numeric', month: '2-digit', day: '2-digit'});
  }
}
