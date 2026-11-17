import { Pipe, PipeTransform } from '@angular/core';
import {
  LUX_IMG,
  COLGATE_IMG,
  HEAD_SHOULDERS_IMG,
  PARLE_G_IMG,
  KELLOGG_IMG
} from '../constants/product-images.data';

@Pipe({
  name: 'productImage',
  standalone: true
})
export class ProductImagePipe implements PipeTransform {
  transform(product: any): string {
    if (!product) {
      return 'https://images.unsplash.com/photo-1542838132-92c53300491e?w=500&auto=format&fit=crop&q=80';
    }

    // If a direct valid external image URL is already present
    if (product.imageUrl && typeof product.imageUrl === 'string' && product.imageUrl.startsWith('http')) {
      return product.imageUrl;
    }

    const name = ((product.name || product.productName || '') + '').toLowerCase();
    const sku = ((product.sku || product.productSku || '') + '').toLowerCase();
    const cat = ((product.categoryName || '') + '').toLowerCase();

    // 1. Soaps & Personal Care - User verified Lux image
    if (name.includes('lux') || name.includes('saboon') || sku.includes('soap') || (name.includes('soap') && !name.includes('dove')) || sku.includes('per-001')) {
      return LUX_IMG;
    }

    // 2. Biscuits & Bakery - User verified Parle-G image
    if (name.includes('parle') || name.includes('biscuit') || name.includes('cookie') || sku.includes('biscuit') || sku.includes('snk-002')) {
      return PARLE_G_IMG;
    }

    // 3. Cold Drinks / Soda
    if (name.includes('coca') || name.includes('coke') || name.includes('pepsi') || name.includes('cola') || sku.includes('snk-004')) {
      return 'https://images.unsplash.com/photo-1622483767028-3f66f32aef97?w=500&auto=format&fit=crop&q=80';
    }

    // 4. Juices & Beverages
    if (name.includes('tropicana') || name.includes('orange') || name.includes('juice') || sku.includes('snk-003')) {
      return 'https://images.unsplash.com/photo-1600271886742-f049cd451bba?w=500&auto=format&fit=crop&q=80';
    }

    // 5. Chips & Crisps
    if (name.includes('lay') || name.includes('chip') || name.includes('wafer') || sku.includes('snk-001')) {
      return 'https://images.unsplash.com/photo-1566478989037-eec170784d0b?w=500&auto=format&fit=crop&q=80';
    }

    // 6. Toothpaste & Dental - User verified Colgate MaxFresh image
    if (name.includes('colgate') || name.includes('maxfresh') || name.includes('paste') || name.includes('tooth') || sku.includes('per-003')) {
      return COLGATE_IMG;
    }

    // 7. Shampoo & Haircare - User verified Head & Shoulders image
    if (name.includes('head') || name.includes('shoulder') || name.includes('shampoo') || sku.includes('per-002')) {
      return HEAD_SHOULDERS_IMG;
    }

    // 8. Rice & Grains
    if (name.includes('rice') || name.includes('basmati') || sku.includes('grc-001')) {
      return 'https://images.unsplash.com/photo-1586201375761-83865001e31c?w=500&auto=format&fit=crop&q=80';
    }

    // 9. Atta & Flour
    if (name.includes('atta') || name.includes('flour') || name.includes('aashirvaad') || sku.includes('grc-003')) {
      return 'https://images.unsplash.com/photo-1509440159596-0249088772ff?w=500&auto=format&fit=crop&q=80';
    }

    // 10. Salt
    if (name.includes('salt') || name.includes('namak') || sku.includes('grc-002')) {
      return 'https://images.unsplash.com/photo-1626082927389-6cd097cdc6ec?w=500&auto=format&fit=crop&q=80';
    }

    // 11. Edible Oil
    if (name.includes('oil') || name.includes('sunflower') || name.includes('fortune') || sku.includes('grc-004')) {
      return 'https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?w=500&auto=format&fit=crop&q=80';
    }

    // 12. Pulses / Dal
    if (name.includes('dal') || name.includes('toor') || name.includes('pulse') || sku.includes('grc-005')) {
      return 'https://images.unsplash.com/photo-1515543237350-b3eea1ec8082?w=500&auto=format&fit=crop&q=80';
    }

    // 13. Milk
    if (name.includes('milk') || name.includes('amul milk') || sku.includes('dai-001')) {
      return 'https://images.unsplash.com/photo-1550583724-b2692b85b150?w=500&auto=format&fit=crop&q=80';
    }

    // 14. Butter
    if (name.includes('butter') || sku.includes('dai-002')) {
      return 'https://images.unsplash.com/photo-1589985270826-4b7bb135bc9d?w=500&auto=format&fit=crop&q=80';
    }

    // 15. Eggs
    if (name.includes('egg') || sku.includes('dai-003')) {
      return 'https://images.unsplash.com/photo-1506976785307-8732e854ad03?w=500&auto=format&fit=crop&q=80';
    }

    // 16. Breakfast Cereals / Corn Flakes - User verified Kellogg's image
    if (name.includes('kellogg') || name.includes('corn flake') || name.includes('cereal') || sku.includes('dai-004')) {
      return KELLOGG_IMG;
    }

    // 17. Dishwash & Cleaning
    if (name.includes('vim') || name.includes('dishwash') || sku.includes('hom-001')) {
      return 'https://images.unsplash.com/photo-1585421514738-01798e348b17?w=500&auto=format&fit=crop&q=80';
    }

    // 18. Disinfectant / Floor Cleaner
    if (name.includes('lizol') || name.includes('disinfectant') || sku.includes('hom-002')) {
      return 'https://images.unsplash.com/photo-1584813470613-5b1c1cad3d69?w=500&auto=format&fit=crop&q=80';
    }

    // 19. LED Bulb
    if (name.includes('bulb') || name.includes('led') || name.includes('philips') || sku.includes('elc-001')) {
      return 'https://images.unsplash.com/photo-1550985543-f47f38aeee65?w=500&auto=format&fit=crop&q=80';
    }

    // 20. Power Extension
    if (name.includes('power strip') || name.includes('syska') || sku.includes('elc-002')) {
      return 'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=500&auto=format&fit=crop&q=80';
    }

    // Category fallbacks
    if (cat.includes('grocery') || cat.includes('staple')) {
      return 'https://images.unsplash.com/photo-1542838132-92c53300491e?w=500&auto=format&fit=crop&q=80';
    }
    if (cat.includes('dairy') || cat.includes('breakfast')) {
      return 'https://images.unsplash.com/photo-1528750997573-59b89d56f4f7?w=500&auto=format&fit=crop&q=80';
    }
    if (cat.includes('snack') || cat.includes('beverage')) {
      return 'https://images.unsplash.com/photo-1566478989037-eec170784d0b?w=500&auto=format&fit=crop&q=80';
    }
    if (cat.includes('personal') || cat.includes('care')) {
      return 'https://images.unsplash.com/photo-1556228720-195a672e8a03?w=500&auto=format&fit=crop&q=80';
    }
    if (cat.includes('home') || cat.includes('kitchen')) {
      return 'https://images.unsplash.com/photo-1585421514738-01798e348b17?w=500&auto=format&fit=crop&q=80';
    }

    return 'https://images.unsplash.com/photo-1542838132-92c53300491e?w=500&auto=format&fit=crop&q=80';
  }
}
